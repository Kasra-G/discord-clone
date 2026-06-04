<script lang="ts">
	import { goto } from '$app/navigation';
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { login } from '$lib/backend.remote';
	import * as schemas from '$lib/schemas';
	import { getUserState } from '$lib/user.svelte';
	import { type RemoteFormField, type RemoteFormFieldValue } from '@sveltejs/kit';
	let submissionError = $state<string>();
	const userState = getUserState();
</script>

{#snippet showErrors(field: RemoteFormField<RemoteFormFieldValue>)}
	{#each field.issues() as issue, i (i)}
		<div class="issue-text">{issue.message}</div>
	{/each}
{/snippet}

<form
	{...login.preflight(schemas.LOGIN)}
	{...login.enhance(async (form) => {
		submissionError = '';
		if (!(await form.submit())) {
			return;
		}

		userState.current = form.result?.user;
		submissionError = form.result?.error;

		if (form.result?.ok) {
			form.element.reset();
			const redirectTo = page.url.searchParams.get('redirectTo') ?? '/chat';
			// eslint-disable-next-line svelte/no-navigation-without-resolve
			await goto(redirectTo);
		}
	})}
	onchange={() => login.validate()}
>
	<h3>Login</h3>
	<div>
		<label>Username <input {...login.fields.username.as('text')} autocomplete="off" /> </label>
		{@render showErrors(login.fields.username)}
	</div>
	<div>
		<label>Password <input {...login.fields._password.as('password')} autocomplete="off" /> </label>
		{@render showErrors(login.fields._password)}
	</div>
	{#if submissionError}
		<div class="issue-text">{submissionError}</div>
	{/if}
	<div class="register-text">
		Don't have an account? <a href={resolve('/register')}>Register</a>
	</div>
	<button disabled={!!login.pending}>Login</button>
</form>

<style>
	.register-text {
		font-size: small;
	}
	.issue-text {
		font-size: small;
		color: var(--red);
	}
</style>
