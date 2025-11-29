import 'dotenv/config';
import crypto from 'crypto';
import express from 'express';
import session from 'express-session';
import helmet from 'helmet';
import morgan from 'morgan';
import fetch from 'cross-fetch';

const {
  PORT = 8081,
  SESSION_SECRET = 'change-me',
  KEYCLOAK_BASE_URL = 'http://localhost:8080/realms/poc',
  KEYCLOAK_CLIENT_ID = 'bff-client',
  KEYCLOAK_TOKEN_URL = `${KEYCLOAK_BASE_URL}/protocol/openid-connect/token`,
  KEYCLOAK_END_SESSION_URL = `${KEYCLOAK_BASE_URL}/protocol/openid-connect/logout`,
  GATEWAY_URL = 'http://localhost:8082'
} = process.env;

const app = express();
app.use(express.json());
app.use(
  session({
    secret: SESSION_SECRET,
    resave: false,
    saveUninitialized: false,
    cookie: {
      httpOnly: true,
      secure: false
    }
  })
);
app.use(helmet());
app.use(morgan('dev'));

type BffSession = session.Session & {
  oauthState?: string;
  pkceVerifier?: string; 
  tokens?: {
    access_token: string;
    refresh_token: string;
    expires_at: number;
    id_token?: string;
  };
};

const baseUrl = () => KEYCLOAK_BASE_URL.replace(/\/$/, '');

app.get('/auth/login', (req, res) => {
  const bffSession = req.session as BffSession;
  const verifier = generateCodeVerifier();
  const challenge = generateCodeChallenge(verifier);
  const state = crypto.randomBytes(16).toString('hex');
  bffSession.pkceVerifier = verifier;
  bffSession.oauthState = state;
  const redirectUri = `${getOrigin(req)}/auth/callback`;
  const authorizeUrl = `${baseUrl()}/protocol/openid-connect/auth?` +
    new URLSearchParams({
      response_type: 'code',
      client_id: KEYCLOAK_CLIENT_ID,
      redirect_uri: redirectUri,
      code_challenge: challenge,
      code_challenge_method: 'S256',
      scope: 'openid profile email',
      state
    }).toString();
  res.redirect(authorizeUrl);
});

app.get('/auth/callback', async (req, res) => {
  const { code, state } = req.query;
  const bffSession = req.session as BffSession;
  if (!code || !state || state !== bffSession.oauthState) {
    return res.status(400).json({ error: 'Invalid state' });
  }
  const redirectUri = `${getOrigin(req)}/auth/callback`;
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: KEYCLOAK_CLIENT_ID,
    code: String(code),
    redirect_uri: redirectUri,
    code_verifier: bffSession.pkceVerifier ?? ''
  });
  const tokenResponse = await fetch(KEYCLOAK_TOKEN_URL!, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body
  });
  if (!tokenResponse.ok) {
    return res.status(502).json({ error: 'Failed to exchange code' });
  }
  const tokens = (await tokenResponse.json()) as {
    access_token: string;
    refresh_token: string;
    expires_in: number;
    id_token: string;
  };
  bffSession.tokens = {
    access_token: tokens.access_token,
    refresh_token: tokens.refresh_token,
    expires_at: Date.now() + tokens.expires_in * 1000,
    id_token: tokens.id_token
  };
  res.redirect('/');
});

app.post('/auth/logout', async (req, res) => {
  const bffSession = req.session as BffSession;
  if (!bffSession.tokens) {
    return res.status(204).send();
  }
  await fetch(KEYCLOAK_END_SESSION_URL!, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: KEYCLOAK_CLIENT_ID,
      refresh_token: bffSession.tokens.refresh_token
    })
  });
  req.session.destroy(() => undefined);
  res.status(204).send();
});

app.get('/session/me', ensureAuthenticated, async (req, res) => {
  const accessToken = await ensureFreshToken(req.session as BffSession);
  const response = await fetch(`${GATEWAY_URL}/api/users/me`, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  if (!response.ok) {
    return res.status(response.status).json({ error: 'Failed to fetch profile' });
  }
  const data = await response.json();
  res.json(data);
});

app.all('/proxy/*', ensureAuthenticated, async (req, res) => {
  const targetPath = req.params[0];
  const accessToken = await ensureFreshToken(req.session as BffSession);
  const upstream = await fetch(`${GATEWAY_URL}/${targetPath}`, {
    method: req.method,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': req.headers['content-type'] ?? ''
    },
    body: ['GET', 'HEAD'].includes(req.method) ? undefined : JSON.stringify(req.body)
  });
  res.status(upstream.status);
  upstream.body?.pipe(res);
});

app.listen(Number(PORT), () => {
  console.log(`BFF listening on :${PORT}`);
});

function ensureAuthenticated(req: express.Request, res: express.Response, next: express.NextFunction) {
  const tokens = (req.session as BffSession).tokens;
  if (!tokens) {
    return res.status(401).json({ error: 'UNAUTHENTICATED' });
  }
  return next();
}

async function ensureFreshToken(bffSession: BffSession) {
  if (!bffSession.tokens) {
    throw new Error('Missing tokens');
  }
  if (Date.now() < bffSession.tokens.expires_at - 5000) {
    return bffSession.tokens.access_token;
  }
  const refreshResponse = await fetch(KEYCLOAK_TOKEN_URL!, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: KEYCLOAK_CLIENT_ID,
      refresh_token: bffSession.tokens.refresh_token
    })
  });
  if (!refreshResponse.ok) {
    throw new Error('Unable to refresh token');
  }
  const refreshed = (await refreshResponse.json()) as {
    access_token: string;
    refresh_token: string;
    expires_in: number;
  };
  bffSession.tokens = {
    ...bffSession.tokens,
    access_token: refreshed.access_token,
    refresh_token: refreshed.refresh_token ?? bffSession.tokens.refresh_token,
    expires_at: Date.now() + refreshed.expires_in * 1000
  };
  return bffSession.tokens.access_token;
}

function generateCodeVerifier() {
  return base64UrlEncode(crypto.randomBytes(32));
}

function generateCodeChallenge(verifier: string) {
  return base64UrlEncode(crypto.createHash('sha256').update(verifier).digest());
}

function base64UrlEncode(buffer: Buffer) {
  return buffer.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function getOrigin(req: express.Request) {
  return `${req.protocol}://${req.get('host')}`;
}

