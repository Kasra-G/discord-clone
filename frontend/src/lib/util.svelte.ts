import { goto } from '$app/navigation';
import { page } from '$app/state';

export function redirectLogin() {
	const returnUrl = page.url.pathname;
	goto(`/login?redirectTo=${encodeURIComponent(returnUrl)}`);
}
