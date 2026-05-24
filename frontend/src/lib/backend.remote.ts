import { query } from '$app/server';
import { PUBLIC_BACKEND_URL } from '$env/static/public';
import z from 'zod';
import type { Message } from './model';

export const getMotd = query(async () => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return await response.text();
});

export const getMessages = query(
	z.object({
		channelId: z.string().default('default'),
		count: z.number().positive().lte(100).default(10)
	}),
	async ({ channelId, count }) => {
		const response = await fetch(
			`${PUBLIC_BACKEND_URL}/channels/${channelId}/messages?count=${count}`
		);
		return (await response.json()) as Message[];
	}
);
