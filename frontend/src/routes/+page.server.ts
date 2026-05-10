import type { PageServerLoad } from "./$types";

export const load: PageServerLoad = async () => {
  const response = await fetch('http://localhost:8080')
	return {
		serverMessage: `hello from backend: ${await response.text()}`
	};
};
