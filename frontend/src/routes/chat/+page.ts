import { PUBLIC_BACKEND_URL } from "$env/static/public";

export const load = async ({ fetch }) => {
	const response = await fetch(PUBLIC_BACKEND_URL);
	return {
		serverMessage: `hello from backend: ${await response.text()}`
	};
};
