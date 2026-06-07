<script module lang="ts">
	import type { RemoteFormField, RemoteFormFieldValue } from '@sveltejs/kit';

	export { showErrors };
</script>

{#snippet showErrors(field: RemoteFormField<RemoteFormFieldValue>)}
	{#each field.issues() as issue, i (i)}
		<div class="issue-text">{issue.message}</div>
	{/each}
{/snippet}

<style>
	.issue-text {
		font-size: small;
		color: var(--red);
	}
</style>
