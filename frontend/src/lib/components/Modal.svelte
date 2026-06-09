<script lang="ts">
	let { showModal = $bindable(false), children } = $props();
</script>

<dialog
	onclose={() => (showModal = false)}
	onclick={({ target, currentTarget }) => {
		if (target === currentTarget) currentTarget.close();
	}}
	{@attach (dialog) => {
		if (showModal) dialog.showModal();
	}}
>
	{@render children()}
</dialog>

<style>
	dialog {
		max-width: 32rem;
		border-radius: 4px;
		border: none;
		padding: 0;
	}
	dialog::backdrop {
		background: rgba(0, 0, 0, 0.3);
	}
	dialog[open] {
		animation: zoom 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
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
