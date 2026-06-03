<script lang="ts">
	import { getUserState } from '$lib/user.svelte';
  const userState = getUserState()
</script>

Logged in: {userState.authenticated()}

<br />
<br />

{#if userState.current}
	Username: {userState.current.username} <br />
	email: {userState.current.email} <br />
	id: {userState.current.id} <br />
	createdAt: {userState.current.createdAt} <br />
	updatedAt: {userState.current.updatedAt} <br />
{/if}
