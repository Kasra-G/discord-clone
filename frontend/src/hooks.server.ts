import { BASE_API_URL } from '$lib/schemas';
import type { Handle } from '@sveltejs/kit';
import { parseSetCookie } from 'set-cookie-parser';

export const handle: Handle = async ({ event, resolve }) => {
	const access_token = event.cookies.get('access_token');
	const refresh_token = event.cookies.get('refresh_token');

	let authenticated = !!access_token && !!refresh_token;

	if (!authenticated && refresh_token) {
		const res = await event.fetch(`${BASE_API_URL}/auth/refresh`, {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: JSON.stringify({
				deviceId: 'my-device'
			})
		});

		authenticated = res.ok;
		if (res.ok) {
			const cookies = parseSetCookie(res);
			cookies.forEach((cookie) => {
				const { name, value, ...options } = cookie;
				event.cookies.set(name, value, { ...options, path: '/', sameSite: 'lax' });
			});
		}
	}
	event.locals.authenticated = authenticated;
	return await resolve(event);
};
