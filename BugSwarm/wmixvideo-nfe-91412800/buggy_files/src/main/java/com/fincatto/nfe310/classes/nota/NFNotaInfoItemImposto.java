package com.fincatto.nfe310.classes.nota;

import java.math.BigDecimal;

import org.simpleframework.xml.Element;

import com.fincatto.nfe310.classes.NFBase;
import com.fincatto.nfe310.validadores.BigDecimalParser;

public class NFNotaInfoItemImposto extends NFBase {

    @Element(name = "vTotTrib", required = false)
    private String valorTotalTributos;

    @Element(name = "ICMS", required = false)
    private NFNotaInfoItemImpostoICMS icms;

    @Element(name = "IPI", required = false)
    private NFNotaInfoItemImpostoIPI ipi;

    @Element(name = "II", required = false)
    private NFNotaInfoItemImpostoImportacao impostoImportacao;

    @Element(name = "ISSQN", required = false)
    private NFNotaInfoItemImpostoISSQN issqn;

    @Element(name = "PIS", required = true)
    private NFNotaInfoItemImpostoPIS pis;

    @Element(name = "PISST", required = false)
    private NFNotaInfoItemImpostoPISST pisst;

    @Element(name = "COFINS", required = false)
    private NFNotaInfoItemImpostoCOFINS cofins;

    @Element(name = "COFINSST", required = false)
    private NFNotaInfoItemImpostoCOFINSST cofinsst;

    public void setIcms(final NFNotaInfoItemImpostoICMS icms) {
        if (this.issqn != null) {
            throw new IllegalStateException("ICMS, IPI e II sao mutuamente exclusivo com ISSQN");
        }
        this.icms = icms;
    }

    public void setIpi(final NFNotaInfoItemImpostoIPI ipi) {
        if (this.issqn != null) {
            throw new IllegalStateException("ICMS, IPI e II sao mutuamente exclusivo com ISSQN");
        }
        this.ipi = ipi;
    }

    public void setImpostoImportacao(final NFNotaInfoItemImpostoImportacao impostoImportacao) {
        if (this.issqn != null) {
            throw new IllegalStateException("ICMS, IPI e II sao mutuamente exclusivo com ISSQN");
        }
        this.impostoImportacao = impostoImportacao;
    }

    public void setIssqn(final NFNotaInfoItemImpostoISSQN issqn) {
        if (this.icms != null || this.ipi != null || this.impostoImportacao != null) {
            throw new IllegalStateException("ICMS, IPI e II sao mutuamente exclusivo com ISSQN");
        }
        this.issqn = issqn;
    }

    public void setPis(final NFNotaInfoItemImpostoPIS pis) {
        this.pis = pis;
    }

    public void setPisst(final NFNotaInfoItemImpostoPISST pisst) {
        this.pisst = pisst;
    }

    public void setCofins(final NFNotaInfoItemImpostoCOFINS cofins) {
        this.cofins = cofins;
    }

    public void setCofinsst(final NFNotaInfoItemImpostoCOFINSST cofinsst) {
        this.cofinsst = cofinsst;
    }

    public void setValorTotalTributos(final BigDecimal valorTotalTributos) {
        this.valorTotalTributos = BigDecimalParser.tamanho15Com2CasasDecimais(valorTotalTributos);
    }

    public String getValorTotalTributos() {
        return this.valorTotalTributos;
    }

    public NFNotaInfoItemImpostoICMS getIcms() {
        return this.icms;
    }

    public NFNotaInfoItemImpostoIPI getIpi() {
        return this.ipi;
    }

    public NFNotaInfoItemImpostoImportacao getImpostoImportacao() {
        return this.impostoImportacao;
    }

    public NFNotaInfoItemImpostoISSQN getIssqn() {
        return this.issqn;
    }

    public NFNotaInfoItemImpostoPIS getPis() {
        return this.pis;
    }

    public NFNotaInfoItemImpostoPISST getPisst() {
        return this.pisst;
    }

    public NFNotaInfoItemImpostoCOFINS getCofins() {
        return this.cofins;
    }

    public NFNotaInfoItemImpostoCOFINSST getCofinsst() {
        return this.cofinsst;
    }
}