<script lang="ts">
	import { userStore } from '$lib/user.svelte';
</script>

Logged in: {userStore.loggedIn}

<br />
<br />

{#if userStore.current}
	Username: {userStore.current.username} <br />
	email: {userStore.current.email} <br />
	id: {userStore.current.id} <br />
	createdAt: {userStore.current.createdAt} <br />
	updatedAt: {userStore.current.updatedAt} <br />
{/if}
