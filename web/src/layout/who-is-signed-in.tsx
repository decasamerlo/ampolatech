import type { Role } from "../api/generated/model";
import { useGetSignedInUser } from "../api/generated/users/users";

// A role the spec adds fails the type-check until it has a label here.
const roleLabels = {
	PHARMACIST: "Farmacêutico",
	SALES_AGENT: "Vendedor",
} satisfies Record<Role, string>;

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
	const { name, role, pharmacy } = query.data;
	return (
		<div className="text-right">
			<p className="font-medium">{name}</p>
			<p className="text-sm text-slate-600">
				<span>{roleLabels[role]}</span> · <span>{pharmacy.name}</span>
			</p>
		</div>
	);
}
