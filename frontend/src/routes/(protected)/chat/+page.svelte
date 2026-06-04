<script lang="ts">
	import { getMessages, getMotd, sendMessage } from '$lib/backend.remote';
	import { messageService } from '$lib/message-service.svelte';
	import type { Message } from '$lib/model';
	import { websocketService } from '$lib/websocket.svelte';

	let inputField: HTMLElement;

	let autoscroll = $state(true);
	let messages = $state<Message[]>([]);

	messageService.onMessage((msg) => messages.push(msg));

	const autoscrollToLast = (elem: HTMLElement) => {
		if (messages.length <= 0) return;
		if (!autoscroll) return;
		elem.lastElementChild?.scrollIntoView();
	};

	const promise = getMessages({ channelId: 'default', count: 100 }).then(
		(res) => (messages = res.reverse())
	);
	await promise;
</script>

<div class="wrapper">
	<h3>Message of the Day: {await getMotd()}</h3>

	<h3>Server status: {websocketService.status}</h3>

	<div class="messages-container">
		<div
			class="chatbox-outer"
			onscroll={({ currentTarget }) => {
				autoscroll =
					currentTarget.scrollHeight - currentTarget.clientHeight - currentTarget.scrollTop < 1;
			}}
		>
			<svelte:boundary>
				<div class="chatbox-inner" {@attach autoscrollToLast}>
					{#each messages as msg (msg.id)}
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
				{#snippet pending()}{/snippet}
			</svelte:boundary>
		</div>
		<form
			{...sendMessage.enhance(async (form) => {
				if (await form.submit().updates()) {
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
				disabled={websocketService.status !== 'CONNECTED'}
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
		flex-direction: column;
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

	.message:first-child {
		margin-top: auto;
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
