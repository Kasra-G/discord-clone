export const ssr = false;
export const prerender = false;
import { redirectLogin } from '$lib/util.svelte';
import type { LayoutServerLoad } from '../$types';

export const load: LayoutServerLoad = async ({ locals, url }) => {
	if (!locals.authenticated) {
		redirectLogin(url.pathname);
	}
};
