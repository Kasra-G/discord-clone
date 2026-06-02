import { command, form, getRequestEvent, query } from '$app/server';
import { PUBLIC_BACKEND_URL } from '$env/static/public';
import { parseSetCookie } from 'set-cookie-parser';
import type { Message, User } from './model';
import * as schemas from './schemas';
import { redirect } from '@sveltejs/kit';

export const getMotd = query(async () => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return await response.text();
});

export const logout = command(async () => {
	checkAuthenticated();
	const event = getRequestEvent();
	await event.fetch(`${schemas.BASE_API_URL}/auth/revoke`, {
		method: 'POST',
		headers: { 'content-type': 'application/json' }
	});
	event.cookies.delete('refresh_token', { path: '/' });
	event.cookies.delete('access_token', { path: '/' });
});

export const sendMessage = command(schemas.SEND_MESSAGE, async ({ message, channelId }) => {
	checkAuthenticated();

	const event = getRequestEvent();
	const res = await event.fetch(`${schemas.BASE_API_URL}/channels/${channelId}/messages`, {
		method: 'POST',
		body: JSON.stringify({ message: message, channelId: channelId }),
		headers: { 'content-type': 'application/json' }
	});

	return {
		status: res.status
	};
});

export const checkAuthenticated = query(async () => {
	const event = getRequestEvent();

	if (!event.locals.authenticated) {
		redirect(307, '/login');
	}
});

export const getMessages = query(schemas.GET_MESSAGES, async ({ channelId, count }) => {
	checkAuthenticated();
	const event = getRequestEvent();
	const response = await event.fetch(
		`${schemas.BASE_API_URL}/channels/${channelId}/messages?count=${count}`
	);
	return (await response.json()) as Message[];
});

export const register = form(schemas.REGISTER, async (data) => {
	checkAuthenticated();
	const event = getRequestEvent();

	const response = await event.fetch(`${schemas.BASE_API_URL}/users/register`, {
		body: JSON.stringify({
			username: data.username,
			email: data.email,
			password: data._password
		}),
		headers: { 'content-type': 'application/json' },
		method: 'POST'
	});

	return await response.json();
});

export const login = form(schemas.LOGIN, async ({ username, _password }) => {
	const event = getRequestEvent();
	const response = await event.fetch(`${schemas.BASE_API_URL}/users/login`, {
		body: JSON.stringify({
			username: username,
			password: _password,
			deviceId: 'my-device'
		}),
		headers: { 'content-type': 'application/json' },
		method: 'POST'
	});

	const cookies = parseSetCookie(response);
	cookies.forEach((cookie) => {
		const { name, value, ...options } = cookie;
		event.cookies.set(name, value, { ...options, path: '/', sameSite: 'lax' });
	});

	const data = await response.json();
	return data.user as User;
});
