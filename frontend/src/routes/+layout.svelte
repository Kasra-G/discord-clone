<script lang="ts">
	import { browser } from '$app/environment';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { ResolvedPathname, Pathname } from '$app/types';
	import favicon from '$lib/assets/favicon.svg';
	import { logout } from '$lib/backend.remote';
	import { setUserState, UserState } from '$lib/user.svelte';

	let { children, data } = $props();

	const userState = new UserState(() => data.authenticated);

	setUserState(userState);

	if (browser) {
		userState.initializeUser();
	}
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

{#snippet navbarButton(pathname: ResolvedPathname, text: string)}
	<form method="GET" action={pathname}>
		<button disabled={resolve(page.url.pathname as unknown as Pathname) === pathname} type="submit"
			>{text}</button
		>
	</form>
{/snippet}

<div class="root">
	<div class="navbar">
		{@render navbarButton(resolve('/'), 'Home')}
		{#if userState.authenticated()}
			<form
				{...logout.enhance(async (form) => {
					if (await form.submit()) {
						await userState.clearUser();
					}
				})}
			>
				<button disabled={!!logout.pending}>Logout</button>
			</form>
		{:else}
			{@render navbarButton(resolve('/login'), 'Login')}
		{/if}

		{@render navbarButton(resolve('/profile'), 'Profile')}
		{@render navbarButton(resolve(`/channels/@me`), 'Chat')}
	</div>

	<div class="body">
		{@render children()}
	</div>
</div>

<style>
	.navbar {
		display: flex;
		border-bottom: 2px solid var(--background-secondary);
	}
	.body {
		flex-grow: 1;
		min-height: 0;
	}
	.root {
		display: flex;
		flex-direction: column;
		height: 100vh;
		width: 100vw;
	}
</style>
