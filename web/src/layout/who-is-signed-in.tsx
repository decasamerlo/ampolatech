import type { Role } from "../api/generated/model";
import { useGetSignedInUser } from "../api/generated/users/users";

// A role the spec adds fails the type-check until it has a label here.
const roleLabels = {
	PHARMACIST: "Farmacêutico",
	SALES_AGENT: "Vendedor",
} satisfies Record<Role, string>;

// The API sends the roles in the order its Role schema lists them, and the
// labels keep it.
function rolesText(roles: Role[]): string {
	if (roles.length === 0) {
		return "Sem perfil";
	}
	return roles.map((role) => roleLabels[role]).join(", ");
}

export function WhoIsSignedIn() {
	const query = useGetSignedInUser();
	if (query.isPending) {
		return <p className="text-sm text-slate-600">Carregando…</p>;
	}
	// TODO(#85): say what went wrong, once the API decides what a problem
	// tells a person.
	if (query.isError) {
		return (
			<p role="alert" className="text-sm text-red-700">
				Não foi possível carregar seus dados. Recarregue a página.
			</p>
		);
	}
	const { name, roles, pharmacy } = query.data;
	return (
		<div className="text-right">
			<p className="font-medium">{name}</p>
			<p className="text-sm text-slate-600">
				<span>{rolesText(roles)}</span> · <span>{pharmacy.name}</span>
			</p>
		</div>
	);
}
