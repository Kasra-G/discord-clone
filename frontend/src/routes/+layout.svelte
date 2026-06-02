<script lang="ts">
	import { page } from '$app/state';
	import favicon from '$lib/assets/favicon.svg';
	import { logout } from '$lib/backend.remote';
	import { userStore } from '$lib/user.svelte';
	import { onMount } from 'svelte';

	let { children } = $props();
	onMount(() => {
		userStore.initialize();
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
		{#if userStore.loggedIn}
			<form
				{...logout.enhance(async (form) => {
					if (await form.submit()) {
						await userStore.clear();
					}
				})}
			>
				<button disabled={!!logout.pending}>Logout</button>
			</form>

			{@render navbarButton('/profile', 'Profile')}
			{@render navbarButton('/chat', 'Chat')}
		{:else}
			{@render navbarButton('/login', 'Login')}
		{/if}
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
