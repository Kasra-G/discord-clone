import { env } from '$env/dynamic/public';
import { env as private_env } from '$env/dynamic/private';
import * as schemas from '$lib/schemas';
import type { Handle, HandleFetch } from '@sveltejs/kit';
import { parseSetCookie } from 'set-cookie-parser';

export const handleFetch: HandleFetch = async ({ request, fetch, event }) => {
	const refresh_token = event.cookies.get('refresh_token');

	if (request.url.startsWith(env.PUBLIC_BACKEND_URL)) {
		const internalUrl = request.url.replace(env.PUBLIC_BACKEND_URL, private_env.BACKEND_URL);

		request = new Request(internalUrl, {
			method: request.method,
			headers: {
				...Object.fromEntries(request.headers.entries()),
				cookie: event.request.headers.get('cookie') || '',
			},
			body: request.body,
		});
	}

	const res = await fetch(request);

	if (res.status !== 401 || !refresh_token) {
		return res;
	}

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
		const cookies = parseSetCookie(refresh);
		cookies.forEach((cookie) => {
			const { name, value, ...options } = cookie;
			event.cookies.set(name, value, { ...options, path: '/', sameSite: 'lax' });
		});
	}

	return await fetch(request.clone());
};

export const handle: Handle = async ({ resolve, event }) => {
	const refresh_token = event.cookies.get('refresh_token');
	const access_token = event.cookies.get('access_token');
	const authenticated = !!access_token;

	event.locals.authenticated = authenticated;
	if (!authenticated && refresh_token) {
		const refresh = await event.fetch(`${schemas.BASE_API_URL}/auth/refresh`, {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: JSON.stringify({
				deviceId: 'my-device',
			}),
		});

		event.locals.authenticated = refresh.ok;
		if (refresh.ok) {
			const cookies = parseSetCookie(refresh);
			cookies.forEach((cookie) => {
				const { name, value, ...options } = cookie;
				event.cookies.set(name, value, { ...options, path: '/', sameSite: 'lax' });
			});
		} else {
			event.cookies.delete('access_token', { path: '/' });
			event.cookies.delete('refresh_token', { path: '/' });
		}
	}
	return await resolve(event);
};
