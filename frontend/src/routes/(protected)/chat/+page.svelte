<script lang="ts">
	import { getMotd, sendMessage } from '$lib/backend.remote';
	import { commandService } from '$lib/command-service.svelte';
	import { websocketService } from '$lib/websocket.svelte';

	let inputField: HTMLElement;
	const messages = $derived(commandService.messages);

	let viewport = $state<HTMLDivElement>();
	let autoscroll = $state(true);

	$effect(() => {
		if (!viewport) return;
		if (messages.length <= 0) return;
		if (!messages[messages.length - 1]) return;
		if (!autoscroll) return;
		viewport.scrollTo(0, viewport.scrollHeight);
	});
	await commandService.loadMessages();
</script>

<div class="wrapper">
	<h3>
		Message of the Day:

		<svelte:boundary>
			{await getMotd()}
			{#snippet pending()}
				Loading...
			{/snippet}
		</svelte:boundary>
	</h3>

	<h3>Server status: {websocketService.status}</h3>

	<div class="messages-container">
		<div
			class="chatbox-outer"
			bind:this={viewport}
			onscroll={() => {
				if (!viewport) return;
				autoscroll = viewport.scrollHeight - viewport.clientHeight - viewport.scrollTop < 1;
			}}
		>
			<div class="chatbox-inner">
				{#each messages.toReversed() as msg (msg.id)}
					<div class="message">
						<div class="message-header">
							<div class="message-username">
								{msg.author.username}
							</div>
							<div class="message-timestamp">
								{new Date(msg.createdAt).toLocaleString([], {
									dateStyle: 'short',
									timeStyle: 'short'
								})}
							</div>
						</div>
						<div>
							{msg.content}
						</div>
					</div>
				{/each}
			</div>
		</div>
		<form
			{...sendMessage.enhance(async (form) => {
				if (await form.submit()) {
					form.element.reset();
					inputField.focus();
				}
			})}
			class="message-input-container"
		>
			<input
				{...sendMessage.fields.message.as('text')}
				class="message-input"
				autocomplete="off"
				placeholder="Type a message..."
				bind:this={inputField}
			/>
		</form>
	</div>
</div>

<style>
	.wrapper {
		display: flex;
		flex-direction: column;
		height: 100%;
	}

	.messages-container {
		display: flex;
		flex-direction: column;
		flex-grow: 1;
		min-height: 0;
	}

	.chatbox-outer {
		overflow-y: auto;
		flex-grow: 1;
		border-top: 1px solid var(--background-accent);
	}

	.chatbox-inner {
		min-height: 100%;
		display: flex;
		flex-direction: column-reverse;
	}

	.message {
		margin: 0px 5px 5px;
		border: 1px solid var(--background-secondary);
		border-radius: 10px;
		padding: 10px;
		background-color: var(--background-primary);
		color: var(--text-muted);
		display: flex;
		flex-direction: column;
		gap: 5px;
	}

	.message-header {
		display: flex;
		gap: 20px;
	}

	.message-username {
		font-size: medium;
		color: var(--text-normal);
	}

	.message-timestamp {
		font-size: small;
	}

	.message-input-container {
		display: flex;
	}

	.message-input {
		flex-grow: 1;
		padding: 15px;
		border-radius: 10px;
		margin: 10px;
		font-size: medium;
	}
	.message-input:focus-visible {
		outline: none;
	}
</style>
