package haw.aip3.haw.fertigungsverwaltung.entities;

import haw.aip3.haw.auftragsverwaltung.entities.KundenAuftrag;
import haw.aip3.haw.base.entities.IBauteil;
import haw.aip3.haw.base.entities.IFertigungsauftrag;
import haw.aip3.haw.base.entities.IKundenAuftrag;
import haw.aip3.haw.produkt.entities.Bauteil;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

@Entity
public class Fertigungsauftrag implements IFertigungsauftrag{

	public Fertigungsauftrag() {

	}

	public Fertigungsauftrag(IKundenAuftrag k) {
		kundenAuftrag = k;
	}

	@Id
	@GeneratedValue
	private Long nr;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH, targetEntity = Bauteil.class)
	private IBauteil bauteil;

	@OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.REFRESH, targetEntity = KundenAuftrag.class)
	private IKundenAuftrag kundenAuftrag;

	public Long getNr() {
		return nr;
	}

	public void setNr(Long nr) {
		this.nr = nr;
	}

	public IBauteil getBauteil() {
		return bauteil;
	}

	public void setBauteil(IBauteil bauteil) {
		this.bauteil = bauteil;
	}

	public IKundenAuftrag getKundenAuftrag() {
		return kundenAuftrag;
	}

	public void setKundenAuftrag(IKundenAuftrag kundenAuftrag) {
		this.kundenAuftrag = kundenAuftrag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bauteil == null) ? 0 : bauteil.hashCode());
		result = prime * result
				+ ((kundenAuftrag == null) ? 0 : kundenAuftrag.hashCode());
		result = prime * result + ((nr == null) ? 0 : nr.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Fertigungsauftrag other = (Fertigungsauftrag) obj;
		if (bauteil == null) {
			if (other.bauteil != null) {
				return false;
			}
		} else if (!bauteil.equals(other.bauteil)) {
			return false;
		}
		if (kundenAuftrag == null) {
			if (other.kundenAuftrag != null) {
				return false;
			}
		} else if (!kundenAuftrag.equals(other.kundenAuftrag)) {
			return false;
		}
		if (nr == null) {
			if (other.nr != null) {
				return false;
			}
		} else if (!nr.equals(other.nr)) {
			return false;
		}
		return true;
	}

}
