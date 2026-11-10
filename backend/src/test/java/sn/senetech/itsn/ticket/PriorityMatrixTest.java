package sn.senetech.itsn.ticket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sn.senetech.itsn.domain.Priority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** TU-01 : matrice impact x urgence (exigence EF-03). */
class PriorityMatrixTest {

    @ParameterizedTest(name = "impact {0} + urgence {1} => {2}")
    @CsvSource({
        "1,1,P1", "1,2,P2", "2,1,P2",
        "1,3,P3", "2,2,P3", "3,1,P3",
        "2,3,P4", "3,2,P4", "3,3,P4"})
    void computesPriorityFromMatrix(int impact, int urgency, Priority expected) {
        assertThat(PriorityMatrix.compute(impact, urgency)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Valeurs hors échelle rejetées")
    void rejectsOutOfRange() {
        assertThatThrownBy(() -> PriorityMatrix.compute(0, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PriorityMatrix.compute(2, 4)).isInstanceOf(IllegalArgumentException.class);
    }
}
