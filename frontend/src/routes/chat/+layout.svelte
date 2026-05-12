<script lang="ts">
	import { messageService } from '$lib/websocket.svelte';

	$effect(() => {
		messageService.connect();
	});

	let { children } = $props();
</script>

{@render children()}
