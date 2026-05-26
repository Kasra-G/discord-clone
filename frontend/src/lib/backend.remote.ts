import { PUBLIC_BACKEND_URL } from '$env/static/public';
import type { Message } from './model';

export const getMotd = async () => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return await response.text();
};

export const getMessages = async (props: { channelId: string; count: number }) => {
	const response = await fetch(
		`${PUBLIC_BACKEND_URL}/channels/${props.channelId}/messages?count=${props.count}`
	);
	return (await response.json()) as Message[];
};
