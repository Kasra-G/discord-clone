import { query } from '$app/server';
import { PUBLIC_BACKEND_URL } from '$env/static/public';

export const getMotd = query(async () => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return await response.text();
});
