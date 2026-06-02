<script lang="ts">
	import Protected from '$lib/Protected.svelte';
	import { userStore } from '$lib/user.svelte';
	import { redirectLogin } from '$lib/util.svelte';

	$effect(() => {
		if (!userStore.loggedIn) {
			redirectLogin();
		}
	});

	let { children } = $props();
</script>

<Protected>
	{@render children()}
</Protected>
