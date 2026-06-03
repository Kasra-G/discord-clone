<script lang="ts">
	import { websocketService } from '$lib/websocket.svelte';
	import { onMount } from 'svelte';

	onMount(() => {
		websocketService.connect();
		return () => websocketService.destroy();
	});

	let { children } = $props();
</script>

{@render children()}
