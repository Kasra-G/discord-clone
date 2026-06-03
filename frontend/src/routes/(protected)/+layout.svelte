<script lang="ts">
	import Protected from '$lib/Protected.svelte';
	import { getUserState } from '$lib/user.svelte';
	import { gotoLogin } from '$lib/util.svelte';

	let { children } = $props();

	$effect(() => {
		if (!getUserState().authenticated()) {
			gotoLogin();
		}
	});
</script>

<Protected>
	{@render children()}
</Protected>
