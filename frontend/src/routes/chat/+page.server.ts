import { BACKEND_URL } from '$env/static/private';

export const load = async () => {
	const response = await fetch(BACKEND_URL);
	return {
		serverMessage: `hello from backend: ${await response.text()}`
	};
};
