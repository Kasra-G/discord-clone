import { BACKEND_URL } from '$env/static/private';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async () => {
	const response = await fetch(BACKEND_URL);
	return {
		serverMessage: `hello from backend: ${await response.text()}`
	};
};
