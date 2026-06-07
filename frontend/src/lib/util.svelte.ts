import { goto } from '$app/navigation';
import { resolve } from '$app/paths';
import { page } from '$app/state';
import { redirect } from '@sveltejs/kit';

export async function gotoLogin() {
	// eslint-disable-next-line svelte/no-navigation-without-resolve
	await goto(getLoginRedirectUrl(page.url.pathname));
}

export function redirectLogin(pathname: string) {
	redirect(307, getLoginRedirectUrl(pathname));
}

export function getLoginRedirectUrl(returnPath: string) {
	return `${resolve('/login')}?redirectTo=${encodeURIComponent(returnPath)}`;
}

export const deep = <T>(arr: T): T => {
	const _ = $state(arr);
	return _;
};
