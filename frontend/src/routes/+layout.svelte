<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import favicon from '$lib/assets/favicon.svg';
	import { logout } from '$lib/backend.remote';
	import { USER_STATE_KEY, UserState } from '$lib/user.svelte';
	import { onMount, setContext } from 'svelte';

	let { children, data } = $props();

	const userState = new UserState(() => data.authenticated);

	setContext(USER_STATE_KEY, userState);

	onMount(() => {
		userState.initializeUser();
	});
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

{#snippet navbarButton(pathname: string, text: string)}
	<form method="GET" action={pathname}>
		<button disabled={page.url.pathname === pathname} type="submit">{text}</button>
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
		{@render navbarButton(resolve('/chat'), 'Chat')}
	</div>

	<div class="body">
		{@render children()}
	</div>
</div>

<style>
	.navbar {
		display: flex;
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
