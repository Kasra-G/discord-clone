<script lang="ts">
	import { beforeNavigate } from '$app/navigation';
	import { getChannel, getMessages, sendMessage } from '$lib/backend.remote';
	import { messageService } from '$lib/message-service.svelte';
	import { deep } from '$lib/util.svelte.js';
	import { websocketService } from '$lib/websocket.svelte';
	import { onMount } from 'svelte';
	import * as schemas from '$lib/schemas';
	import { showErrors } from '$lib/snippets.svelte';

	let { params } = $props();

	let inputField: HTMLElement;
	let messages = $derived(deep(await getMessages({ channelId: params.channelId, count: 100 })));

	beforeNavigate(({ to }) => {
		const channelId = to?.params?.channelId;
		if (channelId) {
			void getMessages({
				channelId: channelId,
				count: 100
			}).refresh();
		}
	});

	onMount(() => {
		return messageService.subscribeOnMessage((msg) => {
			if (msg.channelId === params.channelId) {
				messages.push(msg);
			}
		});
	});

	let scroller = $state<DOMRect>();
	let autoscroll = $state(true);
	const autoscrollToLast = (elem: HTMLElement) => {
		scroller?.width;
		scroller?.height;
		if (messages.length <= 0) return;
		if (!autoscroll) return;
		elem.lastElementChild?.scrollIntoView();
	};

	const getChannelDetails = () => {
		return getChannel({ channelId: params.channelId });
	};
</script>

<div class="messages-container">
	<div class="header">
		<h4>#{(await getChannelDetails()).name}</h4>
		<h5>{(await getChannelDetails()).description}</h5>
	</div>
	<div
		bind:contentRect={scroller}
		class="chatbox-outer"
		onwheel={({ currentTarget }) => {
			autoscroll =
				currentTarget.scrollHeight - currentTarget.clientHeight - currentTarget.scrollTop < 50;
		}}
	>
		<div class="chatbox-inner" {@attach autoscrollToLast}>
			{#each messages as msg (msg.id)}
				<div class="message">
					<div class="header">
						<div class="username">
							{msg.author.username}
						</div>
						<div class="timestamp">
							{new Date(msg.createdAt).toLocaleString([], {
								dateStyle: 'short',
								timeStyle: 'short'
							})}
						</div>
					</div>
					<div class="content">
						{msg.content}
					</div>
				</div>
			{/each}
		</div>
	</div>
	<form
		{...sendMessage.preflight(schemas.SEND_MESSAGE)}
		{...sendMessage.enhance(async (form) => {
			if (await form.submit().updates()) {
				form.element.reset();
				inputField.focus();
			}
		})}
		oninput={() => sendMessage.validate()}
		class="message-input-container"
	>
		<div class="input-error-wrapper">
			{@render showErrors(sendMessage.fields.message)}
		</div>
		<textarea
			{...sendMessage.fields.message.as('text')}
			class="message-input"
			autocomplete="off"
			disabled={websocketService.status !== 'CONNECTED'}
			placeholder="Type a message..."
			bind:this={inputField}
			onkeydown={async (e) => {
				if (e.key === 'Enter' && !e.shiftKey) {
					e.preventDefault();
					await sendMessage.submit();
				}
			}}
		></textarea>
		<input {...sendMessage.fields.channelId.as('hidden', params.channelId)} />
	</form>
</div>

<style>
	.messages-container {
		display: flex;
		flex-direction: column;
		height: 100%;
	}

	.input-error-wrapper {
		margin-bottom: 4px;
		padding-left: 4px;
		font-size: 0.85rem;
	}

	.header {
		padding-left: 10px;
	}

	.chatbox-outer {
		overflow-y: auto;
		flex-grow: 1;
		border-top: 1px solid var(--background-accent);
	}

	.chatbox-inner {
		display: flex;
		flex-direction: column;
		justify-content: flex-end;
	}

	.message {
		margin: 0px 4px 4px;
		border: 1px solid var(--accent);
		border-radius: 12px;
		padding: 12px;
		background-color: var(--background);
		color: var(--foreground);
		display: flex;
		flex-direction: column;
		gap: 4px;

		.header {
			display: flex;
			align-items: center;
			gap: 20px;
			padding-left: 0px;

			.username {
				font-size: large;
				color: var(--primary);
			}
			.timestamp {
				font-size: small;
				color: var(--muted-foreground);
			}
		}
		.content {
			font-size: medium;
			text-wrap-mode: wrap;
			overflow-wrap: break-word;
			white-space: break-spaces;
			text-wrap-style: pretty;
			vertical-align: baseline;
		}
	}

	.message-input-container {
		display: flex;
		flex-direction: column;

		.message-input {
			color: var(--foreground);
			flex-grow: 1;
			min-height: 44px;
			max-height: 200px;

			padding: 12px;
			border-radius: 12px;
			background-color: var(--input);
			margin: 8px;

			font-size: medium;
		}

		textarea {
			font-family: inherit;
			resize: none;
			background: transparent;
			border: none;
			outline: none;
		}
		.message-input::placeholder {
			text-align: center;
		}
	}
</style>
