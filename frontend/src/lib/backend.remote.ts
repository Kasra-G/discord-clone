import { form, getRequestEvent, query } from '$app/server';
import { PUBLIC_BACKEND_URL } from '$env/static/public';
import z from 'zod';
import type { Message } from './model';

export const getMotd = query(async () => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return await response.text();
});

export const getMessages = query(
	z.object({
		channelId: z.string(),
		count: z.number().positive().lte(100)
	}),
	async ({ channelId, count }) => {
		const response = await fetch(
			`${PUBLIC_BACKEND_URL}/channels/${channelId}/messages?count=${count}`
		);
		return (await response.json()) as Message[];
	}
);

export const register = form(
	z.object({
		username: z.string(),
		email: z.email(),
		password: z.string()
	}),
	async (data) => {
		const response = await getRequestEvent().fetch(`${PUBLIC_BACKEND_URL}/users/register`, {
			body: JSON.stringify(data),
			method: 'POST'
		});

		return await response.json();
	}
);
