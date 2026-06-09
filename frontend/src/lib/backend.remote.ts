import { form, getRequestEvent, query } from '$app/server';
import { PUBLIC_BACKEND_URL } from '$env/static/public';
import { parseSetCookie } from 'set-cookie-parser';
import type { Channel, LoginFormResponse, Message } from './model';
import * as schemas from './schemas';
import { invalid, redirect } from '@sveltejs/kit';
import { redirectLogin } from './util.svelte';

export const getMotd = query(async () => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return await response.text();
});

export const getChannels = query(schemas.GET_CHANNELS, async ({ guildId }) => {
	const event = getRequestEvent();
	const response = await event.fetch(`${schemas.BASE_API_URL}/channels/${guildId}`, {
		method: 'GET'
	});

	const channels = (await response.json()) as Channel[];
	return channels;
});

export const getChannel = query(schemas.GET_CHANNEL, async ({ channelId, guildId }) => {
	const event = getRequestEvent();
	const response = await event.fetch(`${schemas.BASE_API_URL}/channels/${guildId}/${channelId}`, {
		method: 'GET'
	});

	return (await response.json()) as Channel;
});

export const createChannel = form(
	schemas.CREATE_CHANNEL,
	async ({ channelName, channelDescription, guildId }) => {
		const event = getRequestEvent();
		const response = await event.fetch(`${schemas.BASE_API_URL}/channels/${guildId}`, {
			method: 'POST',
			headers: { 'content-type': 'application/json' },
			body: JSON.stringify({
				name: channelName,
				description: channelDescription
			})
		});

		if (response.ok) {
			void getChannels({ guildId }).refresh();
		}

		return {
			ok: response.ok,
			error: !response.ok ? (await response.json()).error : undefined
		};
	}
);

export const logout = form(async () => {
	const event = getRequestEvent();
	await event.fetch(`${schemas.BASE_API_URL}/auth/revoke`, {
		method: 'POST',
		headers: { 'content-type': 'application/json' }
	});
	event.cookies.delete('refresh_token', { path: '/' });
	event.cookies.delete('access_token', { path: '/' });
	void getAuthenticated().refresh();
	redirect(308, '/');
});

export const sendMessage = form(schemas.SEND_MESSAGE, async ({ message, channelId, guildId }) => {
	const event = getRequestEvent();
	const res = await event.fetch(
		`${schemas.BASE_API_URL}/channels/${guildId}/${channelId}/messages`,
		{
			method: 'POST',
			body: JSON.stringify({ message: message, channelId: channelId }),
			headers: { 'content-type': 'application/json' }
		}
	);
	return {
		ok: res.ok,
		error: !res.ok
	};
});

export const checkAuthenticated = query(async () => {
	const event = getRequestEvent();

	if (!(await getAuthenticated())) {
		redirectLogin(event.url.pathname);
	}
});

export const getAuthenticated = query(async () => {
	const event = getRequestEvent();
	const access_token = event.cookies.get('access_token');

	return !!access_token;
});

export const getMessages = query(schemas.GET_MESSAGES, async ({ channelId, guildId, count }) => {
	const event = getRequestEvent();
	const response = await event.fetch(
		`${schemas.BASE_API_URL}/channels/${guildId}/${channelId}/messages?count=${count}`
	);
	const messages = (await response.json()) as Message[];
	return messages.toReversed();
});

export const register = form(schemas.REGISTER, async (data, issue) => {
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

		void getAuthenticated().refresh();
		return { ok: true, user: body.user };
	}
);
