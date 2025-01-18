package mk.ukim.finki.filteronemikroservis.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "companies")
@Data
@JsonIgnoreProperties(ignoreUnknown=true)
@NoArgsConstructor
public class StockEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "last_updated")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastUpdated;

    @JsonIgnore
    @OneToMany(mappedBy = "company", fetch = FetchType.EAGER,cascade=CascadeType.ALL)
    @JsonManagedReference
    private List<StockRecordEntity> historicalData;

    public StockEntity(String companyCode) {
        this.companyCode = companyCode;
    }

}