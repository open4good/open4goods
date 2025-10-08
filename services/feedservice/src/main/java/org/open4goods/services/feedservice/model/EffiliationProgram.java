package org.open4goods.services.feedservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EffiliationProgram {

    @JsonProperty("id_affilieur")
    private Integer idAffilieur;

    @JsonProperty("id_programme")
    private Integer idProgramme;

    @JsonProperty("date_debut")
    private String dateDebut; // ou LocalDate si format ISO

    @JsonProperty("date_fin")
    private String dateFin;

    @JsonProperty("description")
    private String description;

    @JsonProperty("siteannonceur")
    private String siteAnnonceur;

    @JsonProperty("urlannonceur")
    private String urlAnnonceur;

    @JsonProperty("id_session")
    private Integer idSession;

    @JsonProperty("url_inscription")
    private String urlInscription;

    @JsonProperty("inscription")
    private String inscription;

    @JsonProperty("categories")
    private String categories;

    @JsonProperty("responsable")
    private String responsable;

    @JsonProperty("tm")
    private String tm;

    @JsonProperty("etat")
    private String etat;

    @JsonProperty("url")
    private String url;

    @JsonProperty("urllo")
    private String urlLogo;

    @JsonProperty("url_tracke")
    private String urlTracke;

    @JsonProperty("nom")
    private String nom;

    @JsonProperty("typecom")
    private String typeCom;

    @JsonProperty("pays")
    private String pays;

    @JsonProperty("dureecookies")
    private Integer dureeCookies;

    @JsonProperty("dureecookiespi")
    private Integer dureeCookiesPi;

    @JsonProperty("scoredb")
    private String scoreDb;

    @JsonProperty("remuneration")
    private String remuneration;

    @JsonProperty("affichage")
    private BigDecimal affichage;

    @JsonProperty("clic")
    private BigDecimal clic;

    @JsonProperty("cpctot")
    private BigDecimal cpcTot;

    @JsonProperty("doubleclic")
    private BigDecimal doubleClic;

    @JsonProperty("ventefixe")
    private BigDecimal venteFixe;

    @JsonProperty("revenue")
    private BigDecimal revenue;

    @JsonProperty("lead")
    private BigDecimal lead;

    @JsonProperty("emailingdedie")
    private String emailingDedie;

    @JsonProperty("nblien")
    private Integer nbLien;

    @JsonProperty("nouveau")
    private String nouveau;

    @JsonProperty("exclusivite")
    private String exclusivite;

    @JsonProperty("nom_challenge")
    private String nomChallenge;

    @JsonProperty("deb_challenge")
    private String debChallenge;

    @JsonProperty("fin_challenge")
    private String finChallenge;

    @JsonProperty("desc_challenge")
    private String descChallenge;

    @JsonProperty("tauxclic")
    private String tauxClic;

    @JsonProperty("transformation")
    private String transformation;

    @JsonProperty("tauxannul")
    private String tauxAnnul;

    @JsonProperty("epc")
    private String epc;

    @JsonProperty("ecpm")
    private String ecpm;

    @JsonProperty("activite")
    private String activite;

    @JsonProperty("evenements")
    private String evenements;

    @JsonProperty("mobile")
    private String mobile;

    @JsonProperty("shop")
    private String shop;

	public Integer getIdAffilieur() {
		return idAffilieur;
	}

	public void setIdAffilieur(Integer idAffilieur) {
		this.idAffilieur = idAffilieur;
	}

	public Integer getIdProgramme() {
		return idProgramme;
	}

	public void setIdProgramme(Integer idProgramme) {
		this.idProgramme = idProgramme;
	}

	public String getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(String dateDebut) {
		this.dateDebut = dateDebut;
	}

	public String getDateFin() {
		return dateFin;
	}

	public void setDateFin(String dateFin) {
		this.dateFin = dateFin;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSiteAnnonceur() {
		return siteAnnonceur;
	}

	public void setSiteAnnonceur(String siteAnnonceur) {
		this.siteAnnonceur = siteAnnonceur;
	}

	public String getUrlAnnonceur() {
		return urlAnnonceur;
	}

	public void setUrlAnnonceur(String urlAnnonceur) {
		this.urlAnnonceur = urlAnnonceur;
	}

	public Integer getIdSession() {
		return idSession;
	}

	public void setIdSession(Integer idSession) {
		this.idSession = idSession;
	}

	public String getUrlInscription() {
		return urlInscription;
	}

	public void setUrlInscription(String urlInscription) {
		this.urlInscription = urlInscription;
	}

	public String getInscription() {
		return inscription;
	}

	public void setInscription(String inscription) {
		this.inscription = inscription;
	}

	public String getCategories() {
		return categories;
	}

	public void setCategories(String categories) {
		this.categories = categories;
	}

	public String getResponsable() {
		return responsable;
	}

	public void setResponsable(String responsable) {
		this.responsable = responsable;
	}

	public String getTm() {
		return tm;
	}

	public void setTm(String tm) {
		this.tm = tm;
	}

	public String getEtat() {
		return etat;
	}

	public void setEtat(String etat) {
		this.etat = etat;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrlLogo() {
		return urlLogo;
	}

	public void setUrlLogo(String urlLogo) {
		this.urlLogo = urlLogo;
	}

	public String getUrlTracke() {
		return urlTracke;
	}

	public void setUrlTracke(String urlTracke) {
		this.urlTracke = urlTracke;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getTypeCom() {
		return typeCom;
	}

	public void setTypeCom(String typeCom) {
		this.typeCom = typeCom;
	}

	public String getPays() {
		return pays;
	}

	public void setPays(String pays) {
		this.pays = pays;
	}

	public Integer getDureeCookies() {
		return dureeCookies;
	}

	public void setDureeCookies(Integer dureeCookies) {
		this.dureeCookies = dureeCookies;
	}

	public Integer getDureeCookiesPi() {
		return dureeCookiesPi;
	}

	public void setDureeCookiesPi(Integer dureeCookiesPi) {
		this.dureeCookiesPi = dureeCookiesPi;
	}

	public String getScoreDb() {
		return scoreDb;
	}

	public void setScoreDb(String scoreDb) {
		this.scoreDb = scoreDb;
	}

	public String getRemuneration() {
		return remuneration;
	}

	public void setRemuneration(String remuneration) {
		this.remuneration = remuneration;
	}

	public BigDecimal getAffichage() {
		return affichage;
	}

	public void setAffichage(BigDecimal affichage) {
		this.affichage = affichage;
	}

	public BigDecimal getClic() {
		return clic;
	}

	public void setClic(BigDecimal clic) {
		this.clic = clic;
	}

	public BigDecimal getCpcTot() {
		return cpcTot;
	}

	public void setCpcTot(BigDecimal cpcTot) {
		this.cpcTot = cpcTot;
	}

	public BigDecimal getDoubleClic() {
		return doubleClic;
	}

	public void setDoubleClic(BigDecimal doubleClic) {
		this.doubleClic = doubleClic;
	}

	public BigDecimal getVenteFixe() {
		return venteFixe;
	}

	public void setVenteFixe(BigDecimal venteFixe) {
		this.venteFixe = venteFixe;
	}

	public BigDecimal getRevenue() {
		return revenue;
	}

	public void setRevenue(BigDecimal revenue) {
		this.revenue = revenue;
	}

	public BigDecimal getLead() {
		return lead;
	}

	public void setLead(BigDecimal lead) {
		this.lead = lead;
	}

	public String getEmailingDedie() {
		return emailingDedie;
	}

	public void setEmailingDedie(String emailingDedie) {
		this.emailingDedie = emailingDedie;
	}

	public Integer getNbLien() {
		return nbLien;
	}

	public void setNbLien(Integer nbLien) {
		this.nbLien = nbLien;
	}

	public String getNouveau() {
		return nouveau;
	}

	public void setNouveau(String nouveau) {
		this.nouveau = nouveau;
	}

	public String getExclusivite() {
		return exclusivite;
	}

	public void setExclusivite(String exclusivite) {
		this.exclusivite = exclusivite;
	}

	public String getNomChallenge() {
		return nomChallenge;
	}

	public void setNomChallenge(String nomChallenge) {
		this.nomChallenge = nomChallenge;
	}

	public String getDebChallenge() {
		return debChallenge;
	}

	public void setDebChallenge(String debChallenge) {
		this.debChallenge = debChallenge;
	}

	public String getFinChallenge() {
		return finChallenge;
	}

	public void setFinChallenge(String finChallenge) {
		this.finChallenge = finChallenge;
	}

	public String getDescChallenge() {
		return descChallenge;
	}

	public void setDescChallenge(String descChallenge) {
		this.descChallenge = descChallenge;
	}

	public String getTauxClic() {
		return tauxClic;
	}

	public void setTauxClic(String tauxClic) {
		this.tauxClic = tauxClic;
	}

	public String getTransformation() {
		return transformation;
	}

	public void setTransformation(String transformation) {
		this.transformation = transformation;
	}

	public String getTauxAnnul() {
		return tauxAnnul;
	}

	public void setTauxAnnul(String tauxAnnul) {
		this.tauxAnnul = tauxAnnul;
	}

	public String getEpc() {
		return epc;
	}

	public void setEpc(String epc) {
		this.epc = epc;
	}

	public String getEcpm() {
		return ecpm;
	}

	public void setEcpm(String ecpm) {
		this.ecpm = ecpm;
	}

	public String getActivite() {
		return activite;
	}

	public void setActivite(String activite) {
		this.activite = activite;
	}

	public String getEvenements() {
		return evenements;
	}

	public void setEvenements(String evenements) {
		this.evenements = evenements;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getShop() {
		return shop;
	}

	public void setShop(String shop) {
		this.shop = shop;
	}


}
