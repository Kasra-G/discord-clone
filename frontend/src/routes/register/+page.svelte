<script lang="ts">
	import { register } from '$lib/backend.remote';
	import * as schemas from '$lib/schemas';
	import type { RemoteFormField, RemoteFormFieldValue } from '@sveltejs/kit';
</script>

{#snippet showErrors(field: RemoteFormField<RemoteFormFieldValue>)}
	{#each field.issues() as issue}
		<div class="issue-text">{issue.message}</div>
	{/each}
{/snippet}

<form {...register.preflight(schemas.REGISTER)} oninput={() => register.validate()}>
	<h3>Register</h3>
	<div>
		<label>Email <input {...register.fields.email.as('email')} /> </label>
		{@render showErrors(register.fields.email)}
	</div>
	<div>
		<label>Username <input {...register.fields.username.as('text')} /> </label>
		{@render showErrors(register.fields.username)}
	</div>
	<div>
		<label>Password <input {...register.fields.password.as('password')} /> </label>
		{@render showErrors(register.fields.password)}
	</div>
	<div>
		<label>Confirm Password <input {...register.fields.confirmPassword.as('password')} /> </label>
		{@render showErrors(register.fields.confirmPassword)}
	</div>
	<div class="login-text">
		Already have an account? <a href="/login">Login</a>
	</div>
	<button>Register</button>
</form>

<style>
	.login-text {
		font-size: small;
	}
	.issue-text {
		font-size: small;
		color: red;
	}
</style>
