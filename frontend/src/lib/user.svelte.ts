import { getContext } from 'svelte';
import type { User } from './model';

const KEY = 'app_user';

export class UserState {
	current = $state<User>();

	constructor(readonly authenticated: () => boolean) {}

	initializeUser() {
		const saved = localStorage.getItem(KEY);
		if (saved) {
			try {
				this.current = JSON.parse(saved);
			} catch (_) {
				console.log('Clearing broken local storage user');
				localStorage.removeItem(KEY);
			}
		}

		$effect(() => {
			if (this.current) {
				localStorage.setItem(KEY, JSON.stringify(this.current));
			}
		});
	}

	async clearUser() {
		this.current = undefined;
		localStorage.removeItem(KEY);
	}
}

export const USER_STATE_KEY = Symbol('user_state');

export const getUserState = () => getContext<UserState>(USER_STATE_KEY);
