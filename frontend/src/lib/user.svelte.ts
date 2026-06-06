import { createContext } from 'svelte';
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
			} catch {
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

export const [getUserState, setUserState] = createContext<UserState>();
