<script lang="ts">
	import Protected from '$lib/Protected.svelte';
	import { getUserState } from '$lib/user.svelte';
	import { gotoLogin } from '$lib/util.svelte';
	import { websocketService } from '$lib/websocket.svelte';
	import { onMount } from 'svelte';

	let { children } = $props();

	onMount(() => {
		websocketService.connect();
		return () => websocketService.destroy();
	});

	$effect(() => {
		if (!getUserState().authenticated()) {
			gotoLogin().catch(console.error);
		}
	});
</script>

<Protected>
	{@render children()}
</Protected>
