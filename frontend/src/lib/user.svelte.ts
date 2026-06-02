import { logout } from './backend.remote';
import type { User } from './model';

const KEY = 'app_user';

class UserStore {
	current = $state<User>();
	readonly loggedIn = $derived(this.current !== undefined);

	initialize() {
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

	async clear() {
		this.current = undefined;
		localStorage.removeItem(KEY);
		await logout();
	}
}

export const userStore = new UserStore();
