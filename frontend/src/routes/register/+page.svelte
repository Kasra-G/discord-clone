<script lang="ts">
	import { resolve } from '$app/paths';
	import { register } from '$lib/backend.remote';
	import * as schemas from '$lib/schemas';
	import { showErrors } from '$lib/snippets.svelte';
</script>

<form {...register.preflight(schemas.REGISTER)} oninput={() => register.validate()}>
	<h3>Register</h3>
	<div>
		<label>Email <input {...register.fields.email.as('email')} autocomplete="off" /> </label>
		{@render showErrors(register.fields.email)}
	</div>
	<div>
		<label>Username <input {...register.fields.username.as('text')} autocomplete="off" /> </label>
		{@render showErrors(register.fields.username)}
	</div>
	<div>
		<label>Password <input {...register.fields._password.as('password')} /> </label>
		{@render showErrors(register.fields._password)}
	</div>
	<div>
		<label>Confirm Password <input {...register.fields._confirmPassword.as('password')} /> </label>
		{@render showErrors(register.fields._confirmPassword)}
	</div>
	<div class="login-text">
		Already have an account? <a href={resolve('/login')}>Login</a>
	</div>
	<button>Register</button>
</form>

<style>
	.login-text {
		font-size: small;
	}
</style>
