import { env } from '$env/dynamic/public';
import { env as private_env } from '$env/dynamic/private';
import * as schemas from '$lib/schemas';
import type { Handle, HandleFetch } from '@sveltejs/kit';
import { parseSetCookie } from 'set-cookie-parser';
import { logger } from '$lib/server/logging';

export const handleFetch: HandleFetch = async ({ request, fetch, event }) => {
	const refresh_token = event.cookies.get('refresh_token');
	const correlationId = event.request.headers.get('x-request-id') || crypto.randomUUID();

	if (request.url.startsWith(env.PUBLIC_BACKEND_URL)) {
		const internalUrl = request.url.replace(env.PUBLIC_BACKEND_URL, private_env.BACKEND_URL);

		logger.debug(
			{
				event: 'url_rewrite',
				publicUrl: env.PUBLIC_BACKEND_URL,
				internalUrl: private_env.BACKEND_URL,
				method: request.method,
				correlationId,
			},
			'Replacing public backend target with internal URL',
		);
		request = new Request(internalUrl, {
			method: request.method,
			headers: {
				...Object.fromEntries(request.headers.entries()),
				cookie: event.request.headers.get('cookie') || '',
			},
			body: request.body,
			duplex: 'half',
		});
	}

	try {
		const res = await fetch(request);

		if (res.status !== 401 || !refresh_token) {
			return res;
		}

		logger.debug(
			{
				event: 'token_refresh_attempt',
				trigger: 'fetch_401',
				url: request.url,
				correlationId,
			},
			'User not authenticated, attempting to refresh access token via handleFetch',
		);
		const refresh = await fetch(`${private_env.BACKEND_URL}/api/auth/refresh`, {
			method: 'POST',
			headers: {
				'content-type': 'application/json',
				cookie: event.request.headers.get('cookie') || '',
			},
			body: JSON.stringify({
				deviceId: 'my-device',
			}),
		});

		event.locals.authenticated = refresh.ok;
		if (refresh.ok) {
			logger.info(
				{ event: 'token_refresh_success', correlationId },
				'Successfully refreshed token via handleFetch',
			);
			const cookies = parseSetCookie(refresh);
			cookies.forEach((cookie) => {
				const { name, value, ...options } = cookie;
				event.cookies.set(name, value, { ...options, path: '/', sameSite: 'lax' });
			});
		} else {
			logger.debug(
				{
					event: 'token_refresh_failed',
					statusCode: refresh.status,
					correlationId,
				},
				'Token refresh failed during API route fetch interception',
			);
		}

		return await fetch(request.clone());
	} catch (error) {
		logger.error(
			{
				event: 'fetch_interception_error',
				url: request.url,
				error,
				correlationId,
			},
			'Uncaught exception occurred during fetch lifecycle execution',
		);
		throw error;
	}
};

export const handle: Handle = async ({ resolve, event }) => {
	const refresh_token = event.cookies.get('refresh_token');
	const access_token = event.cookies.get('access_token');
	const authenticated = !!access_token;

	const url = event.url.pathname;
	const correlationId = crypto.randomUUID();

	event.locals.authenticated = authenticated;
	if (!authenticated && refresh_token) {
		logger.debug(
			{
				event: 'token_refresh_attempt',
				trigger: 'missing_access_token',
				url,
				correlationId,
			},
			'User missing valid access token, attempting refresh validation loop',
		);

		try {
			const refresh = await event.fetch(`${schemas.BASE_API_URL}/auth/refresh`, {
				method: 'POST',
				headers: { 'content-type': 'application/json' },
				body: JSON.stringify({
					deviceId: 'my-device',
				}),
			});

			event.locals.authenticated = refresh.ok;
			if (refresh.ok) {
				logger.info(
					{ event: 'token_refresh_success', url, correlationId },
					'Successfully validated and refreshed request token credentials',
				);
				const cookies = parseSetCookie(refresh);
				cookies.forEach((cookie) => {
					const { name, value, ...options } = cookie;
					event.cookies.set(name, value, { ...options, path: '/', sameSite: 'lax' });
				});
			} else {
				logger.debug(
					{
						event: 'token_refresh_failed',
						url,
						statusCode: refresh.status,
						correlationId,
					},
					'Token refresh exchange denied, clearing active session references',
				);
				event.cookies.delete('access_token', { path: '/' });
				event.cookies.delete('refresh_token', { path: '/' });
			}
		} catch (error) {
			logger.error(
				{
					event: 'server_handle_refresh_error',
					url,
					error,
					correlationId,
				},
				'Server hook encountered unexpected error during session resolution execution',
			);
		}
	}

	return await resolve(event);
};
