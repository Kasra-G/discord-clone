import { goto } from '$app/navigation';
import { page } from '$app/state';
import { redirect } from '@sveltejs/kit';

export function gotoLogin() {
	goto(getLoginRedirectUrl(page.url.pathname));
}

export function redirectLogin(pathname: string) {
	redirect(307, getLoginRedirectUrl(pathname));
}

export function getLoginRedirectUrl(returnPath: string) {
	return `/login?redirectTo=${encodeURIComponent(returnPath)}`;
}
