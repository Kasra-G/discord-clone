<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		showModal = $bindable(false),
		children
	}: {
		showModal: boolean;
		children: Snippet;
	} = $props();
</script>

<dialog
	onclose={() => (showModal = false)}
	closedby="any"
	{@attach (dialog) => {
		if (showModal) {
			dialog.showModal();
		} else {
			dialog.close();
		}
	}}
>
	<button onclick={() => (showModal = false)}>x</button>
	<div class="modal-content">
		{@render children()}
	</div>
</dialog>

<style>
	dialog {
		max-width: 100vw;
		max-height: 100vh;
		border: none;
		padding: 0;
		background-color: var(--popover);
		border-radius: 1.5rem;
	}
	dialog::backdrop {
		background: rgba(0, 0, 0, 0.3);
	}
	dialog[open] {
		animation: zoom 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
	}
	.modal-content {
		margin: 1rem;
	}
	button {
		box-sizing: border-box;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		position: absolute;
		color: var(--foreground);
		border: none;
		cursor: pointer;
		font-size: x-large;
		padding: 1rem;
		margin: 1rem;
		background-color: transparent;
		border-radius: 0.25rem;
		width: 1rem;
		height: 1rem;
		top: 0;
		right: 0;
	}
	button:hover {
		background-color: var(--accent);
	}
	@keyframes zoom {
		from {
			transform: scale(0.95);
		}
		to {
			transform: scale(1);
		}
	}
	dialog[open]::backdrop {
		animation: fade 0.2s ease-out;
	}
	@keyframes fade {
		from {
			opacity: 0;
		}
		to {
			opacity: 1;
		}
	}
</style>
