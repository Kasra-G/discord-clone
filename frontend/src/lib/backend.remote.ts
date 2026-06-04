import { form, getRequestEvent, query } from '$app/server';
import { PUBLIC_BACKEND_URL } from '$env/static/public';
import { parseSetCookie } from 'set-cookie-parser';
import type { LoginFormResponse, Message } from './model';
import * as schemas from './schemas';
import { invalid, redirect } from '@sveltejs/kit';
import { redirectLogin } from './util.svelte';

export const getMotd = query(async () => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return await response.text();
});

export const logout = form(async () => {
	checkAuthenticated();
	const event = getRequestEvent();
	await event.fetch(`${schemas.BASE_API_URL}/auth/revoke`, {
		method: 'POST',
		headers: { 'content-type': 'application/json' }
	});
	event.cookies.delete('refresh_token', { path: '/' });
	event.cookies.delete('access_token', { path: '/' });
	redirect(308, '/');
});

export const sendMessage = form(schemas.SEND_MESSAGE, async ({ message, channelId }) => {
	checkAuthenticated();
	const event = getRequestEvent();
	await event.fetch(`${schemas.BASE_API_URL}/channels/${channelId}/messages`, {
		method: 'POST',
		body: JSON.stringify({ message: message, channelId: channelId }),
		headers: { 'content-type': 'application/json' }
	});
});

export const checkAuthenticated = query(async () => {
	const event = getRequestEvent();

	if (!getAuthenticated()) {
		redirectLogin(event.url.pathname);
	}
});

export const getAuthenticated = query(async () => {
	return getRequestEvent().locals.authenticated;
});

export const getMessages = query(schemas.GET_MESSAGES, async ({ channelId, count }) => {
	checkAuthenticated();
	const event = getRequestEvent();
	const response = await event.fetch(
		`${schemas.BASE_API_URL}/channels/${channelId}/messages?count=${count}`
	);
	return (await response.json()) as Message[];
});

export const register = form(schemas.REGISTER, async (data, issue) => {
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

	if (!response.ok) {
		invalid(issue.username('username is not available'));
	}

	return await response.json();
});

export const login = form(
	schemas.LOGIN,
	async ({ username, _password }): Promise<LoginFormResponse> => {
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

		const body = await response.json();

		if (!response.ok) {
			return { ok: false, error: body.error };
		}

		const cookies = parseSetCookie(response);
		cookies.forEach((cookie) => {
			const { name, value, ...options } = cookie;
			event.cookies.set(name, value, { ...options, path: '/', sameSite: 'lax' });
		});

		return { ok: true, user: body.user };
	}
);
