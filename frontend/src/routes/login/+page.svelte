<script lang="ts">
	import { login } from '$lib/backend.remote';
	import * as schemas from '$lib/schemas';
	import type { RemoteFormField, RemoteFormFieldValue } from '@sveltejs/kit';
</script>

{#snippet showErrors(field: RemoteFormField<RemoteFormFieldValue>)}
	{#each field.issues() as issue}
		<div class="issue-text">{issue.message}</div>
	{/each}
{/snippet}

<form {...login.preflight(schemas.LOGIN)} onchange={() => login.validate()}>
	<h3>Login</h3>
	<div>
		<label>Username <input {...login.fields.username.as('text')} /> </label>
		{@render showErrors(login.fields.username)}
	</div>
	<div>
		<label>Password <input {...login.fields._password.as('password')} /> </label>
		{@render showErrors(login.fields._password)}
	</div>
	<div class="register-text">
		Don't have an account? <a href="/register">Register</a>
	</div>
	<button>Login</button>
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
