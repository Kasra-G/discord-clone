import { form, getRequestEvent, query } from '$app/server';
import { PUBLIC_BACKEND_URL } from '$env/static/public';
import type { Message } from './model';
import * as schemas from './schemas';

export const getMotd = query(async () => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return await response.text();
});

export const getMessages = query(schemas.GET_MESSAGES, async ({ channelId, count }) => {
	const response = await fetch(
		`${PUBLIC_BACKEND_URL}/channels/${channelId}/messages?count=${count}`
	);
	return (await response.json()) as Message[];
});
export const register = form(schemas.REGISTER, async (data) => {
	const response = await getRequestEvent().fetch(`${PUBLIC_BACKEND_URL}/users/register`, {
		body: JSON.stringify(data),
		method: 'POST'
	});

	return await response.json();
});

export const login = form(schemas.LOGIN, async (data) => {
	const response = await getRequestEvent().fetch(`${PUBLIC_BACKEND_URL}/users/login`, {
		body: JSON.stringify(data),
		method: 'POST'
	});

	return await response.json();
});
