<script lang="ts">
	import { resolve } from '$app/paths';
	import { getSelfGuilds, selfCreateGuild, selfJoinGuild } from '$lib/backend.remote';
	import * as schemas from '$lib/schemas';
	import type { Guild } from '$lib/model';
	import { getUserState } from '$lib/user.svelte';
	import Modal from './Modal.svelte';

	let { class: className = '' } = $props();
	let showCreateGuildModal = $state(false);
	let showJoinGuildModal = $state(false);
</script>

{#snippet guildButton(guild: Guild)}
	<li><a href={resolve(`/channels/${guild.id}/`)}>{guild.name}</a></li>
{/snippet}

<Modal bind:showModal={showCreateGuildModal}>
	<form
		{...selfCreateGuild.preflight(schemas.SELF_CREATE_GUILD)}
		{...selfCreateGuild.enhance(async (form) => {
			if (await form.submit()) {
				form.element.reset();
				showCreateGuildModal = false;
			}
		})}
	>
		<input
			{...selfCreateGuild.fields.name.as('text', `${getUserState().current?.username}'s Server`)}
		/>
		<input
			{...selfCreateGuild.fields.description.as(
				'text',
				`Server for ${getUserState().current?.username}`
			)}
		/>
		<button> Submit </button>
	</form>
</Modal>

<Modal bind:showModal={showJoinGuildModal}>
	<form
		{...selfJoinGuild.preflight(schemas.SELF_JOIN_GUILD)}
		{...selfJoinGuild.enhance(async (form) => {
			if (await form.submit()) {
				form.element.reset();
				showJoinGuildModal = false;
			}
		})}
	>
		<input {...selfJoinGuild.fields.guildId.as('text')} />
		<button> Submit </button>
	</form>
</Modal>

<aside class="root {className}">
	<div class="scroll-wrapper">
		<ol>
			{#each await getSelfGuilds() as guild (guild.id)}
				{@render guildButton(guild)}
			{/each}
			<li><button onclick={() => (showCreateGuildModal = true)}>Create</button></li>
			<li><button onclick={() => (showJoinGuildModal = true)}>Join</button></li>
		</ol>
	</div>
</aside>

<style>
	.root {
		/* border: 1px solid var(--accent); */
		/* border-radius: 16px; */
		height: 100%;
		width: 100%;
		overflow: scroll;
	}
	.scroll-wrapper {
		display: flex;
		justify-content: center;
	}
	ol {
		display: flex;
		flex-direction: column;
		list-style-type: none;
		margin-left: 0px;
		padding-left: 0px;
		gap: 8px;
	}
	li {
		display: flex;
		width: 48px;
		height: 48px;
		border: 1px solid var(--accent);
		border-radius: 16px;
		justify-content: center;
		align-items: center;
	}
	a {
		text-decoration: none;
		color: var(--foreground);
	}
</style>
